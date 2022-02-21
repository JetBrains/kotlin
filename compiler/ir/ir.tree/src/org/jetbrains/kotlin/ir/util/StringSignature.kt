/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.util

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.name.FqName

private fun Char.isValidId(): Boolean {
    return this == '_' || this == '\\' || this == '-' || this.isLetterOrDigit()
}

private fun Char.isValidIdStart(): Boolean {
    return this == '_' || this == '\\' || this == '<' || this.isLetter()
}

class StringSignature private constructor(val value: String, b: StringSignature.() -> ParsedSignature) {

    constructor(_value: String) : this(_value, { parseSignature() })

    constructor(_pkg: String, _cls: String) : this("$_pkg/$_cls")

    override fun hashCode(): Int {
        return value.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return other is StringSignature && value == other.value
    }

    val isLocal: Boolean get() = value[0] == '$'

    fun containerSignature(): StringSignature {
        assert(!isLocal)

        val container = parsedSignature.containerSignature()
        return StringSignature(container.asString()) { container }
    }

    fun topLevelSignature(): StringSignature {
        assert(!isLocal)

        var cur = this
        var cont = containerSignature()

        while (cur != cont) {
            cur = cont
            cont = cont.containerSignature()
        }

        return cur
    }

    fun nameSegments(): List<String> {
        assert(!isLocal)
        return declarationFqName.split('.')
    }

    val parsedSignature by lazy { b() }

    private fun String.deescape(): String {
        val s = this
        return buildString {
            var i = 0
            while (i < s.length) {
                if (s[i] == '\\') {
                    ++i
                }
                append(s[i++])
            }
        }
    }

    val declarationFqName: String
        get() = topLevelPrefix().third.deescape()

    val shortName: String get() = declarationFqName.substringAfterLast('.')

    fun packageFqName(): FqName = FqName(topLevelPrefix().second)

    private fun topLevelPrefix() = StringSignatureParser(value).parseTopLevelPrefix()

    fun localIndex(): Int {
        assert(isLocal)
        return Integer.parseInt(value.substring(1))
    }


    class StringSignatureParser(private val value: String) {
        private var idx = 0

        private fun parseIdentificator(): String {
            return buildString {

                checkNotEnd()
                val isSpecial = value[idx] == '<'

                if (isSpecial) {
                    append(value[idx++])
                    checkNotEnd()
                }

                do {
                    if (value[idx] == '\\') {
                        append(value[idx++])
                        checkNotEnd()
                    }
                    append(value[idx++])
                } while (idx < value.length && value[idx].isValidId())

                if (isSpecial) {
                    consume('>')
                    append('>')
                }
            }
        }

        private fun parseNumber(): String {
            return buildString {
                while (idx < value.length && value[idx].isDigit()) {
                    append(value[idx++])
                }
            }
        }

        private fun parseType(): ParsedSignature.Type {
            if (consumeIf2('^', 'd')) return ParsedSignature.Type.DynamicType
            if (consumeIf2('^', 'e')) return ParsedSignature.Type.ErrorType
            if (consumeIf('^')) error("Unknown type kind \"^${value[idx]}\" in $value")

            var isTypeParameter = false
            val classifier = buildString {
                if (value[idx] == '{') {
                    val idx2 = value.indexOf('}', idx)
                    assert(idx2 > 0)
                    assert(idx2 > idx + 3) { value }
                    append(value.substring(idx, idx2 + 1))
                    idx = idx2 + 1
                    isTypeParameter = true
                } else {
                    val (pkg, cls) = parseClassId()
                    if (cls[0] != '$') {
                        append(pkg)
                        append('/')
                    }
                    append(cls)
                }
            }

            val arguments = mutableListOf<ParsedSignature.Type.TypeArgument>()

            if (consumeIf(MangleConstant.TYPE_ARGUMENTS.prefix)) {
                assert(!isTypeParameter)

                do {
                    consumeIf(MangleConstant.TYPE_ARGUMENTS.separator)
                    if (consumeIf(MangleConstant.STAR_MARK)) {
                        arguments.add(ParsedSignature.Type.TypeArgument.Star)
                    } else {
                        var variance = ""
                        if (consumeIf('+')) variance = "+"
                        if (consumeIf('-')) variance = "-"
                        val type = parseType()
                        arguments.add(ParsedSignature.Type.TypeArgument.Arg(variance, type))
                    }
                } while (idx < value.length && value[idx] == MangleConstant.TYPE_ARGUMENTS.separator)
                consume(MangleConstant.TYPE_ARGUMENTS.suffix)
            }

            val nullability = if (consumeIf('?')) "?" else ""

            return ParsedSignature.Type.SimpleType(classifier, arguments, nullability)
        }

        private fun parseClassId(): Pair<String, String> {

            if (consumeIf('$')) {
                return "" to buildString {
                    append('$')
                    append(parseNumber())
                }
            }

            val pkg = buildString {
                while (idx < value.length && value[idx] != '/') {
                    append(parseIdentificator())
                    if (consumeIf('.')) append('.')
                }
            }

            consume('/')

            checkNotEnd()

            fun nextFqnPart(): Boolean =
                idx < value.length && value[idx] == '.' && ((idx + 1) == value.length || value[idx + 1] != '.')

            val cls = buildString {
                do {
                    if (consumeIf('.')) append('.')
                    append(parseIdentificator())
                } while (nextFqnPart())
            }

            return pkg to cls
        }

        private val isLocalSig: Boolean get() = value[0] == '$'

        fun parseSignature(): ParsedSignature {
            return if (isLocalSig) parseLocalMemberSignature() else parseTopLevelSignature()
        }

        fun parseTopLevelPrefix(): Triple<String?, String, String> {
            assert(!isLocalSig)
            val fileOrPackage = buildString {
                while (idx < value.length && value[idx] != MangleConstant.NAME_SEPARATOR) {
                    append(parseIdentificator())
                    if (consumeIf('.')) append('.')
                }
            }

            consume('/')

            val packageOrClass = buildString {
                do {
                    if (consumeIf('.')) {
                        if (value[idx].isValidIdStart()) {
                            append('.')
                        } else break
                    }
                    append(parseIdentificator())
                } while (idx < value.length && value[idx] == '.')
            }

            return if (consumeIf(MangleConstant.NAME_SEPARATOR)) {
                val declarationName = buildString {
                    do {
                        if (consumeIf('.')) {
                            if (value[idx].isValidIdStart()) {
                                append('.')
                            } else break
                        }
                        append(parseIdentificator())
                    } while (idx < value.length && value[idx] == '.')
                }
                Triple(fileOrPackage, packageOrClass, declarationName)
            } else {
                Triple(null, fileOrPackage, packageOrClass)
            }
        }

        private fun consume(c: Char) {
            assert(idx < value.length) { value }
            if (value[idx++] != c)
                error("Expecting '$c' at position #$idx in \"$value\"")
        }

        private fun consumeIf(c: Char): Boolean {
            if (idx < value.length && value[idx] == c) {
                ++idx
                return true
            }
            return false
        }

        private fun consumeIf2(c1: Char, c2: Char): Boolean {
            if (idx + 1 < value.length && value[idx] == c1) {
                if (value[idx + 1] == c2) {
                    idx += 2
                    return true
                }
            }
            return false
        }

        private fun checkNotEnd() {
            if (idx == value.length)
                error("Signature $value is corrupted")
        }

        private fun checkIsOver() {
            if (idx != value.length)
                error("Expected to signature \"$value\" is over at #$idx")
        }

        private fun parseLocalMemberSignature(): ParsedSignature.LocalMemberSignature {
            consume('$')
            assert(value[idx].isDigit()) { value }

            val numS = parseNumber()
            assert(numS.isNotEmpty()) { value }
            consume('|')

            return ParsedSignature.LocalMemberSignature(Integer.parseInt(numS), value.substring(idx))
        }

        private fun parseTopLevelSignature(): ParsedSignature.TopLevelSignature {
            val (fileName, pkgName, declarationName) = parseTopLevelPrefix()

            if (idx == value.length) {
                assert(fileName == null) { "class cannot have file prefix $fileName, signature \"$value\" is corrupted" }
                return ParsedSignature.ClassSignature(pkgName, declarationName)
            }

            val extensionReceiver = if (consumeIf(MangleConstant.EXTENSION_RECEIVER_MARK)) {
                parseType().also {
                    consume(MangleConstant.EXTENSION_RECEIVER_MARK)
                }
            } else null

            checkNotEnd()

            var returnType: ParsedSignature.Type? = null
            val valueParameters = mutableListOf<ParsedSignature.FunctionSignature.ValueParameter>()
            var isProperty = false

            when (value[idx++]) {
                MangleConstant.RETURN_TYPE_MARK -> {
                    // property
                    checkNotEnd()
                    returnType = parseType()
                    isProperty = true
                }
                MangleConstant.FIELD_MARK -> {
                    // class field
                    assert(extensionReceiver == null) { value }
                    assert(fileName == null) { value }

                    val fieldName = parseIdentificator()

                    assert(fieldName.isNotEmpty()) { value }
                    checkIsOver()

                    return ParsedSignature.FieldSignature(ParsedSignature.ClassSignature(pkgName, declarationName), fieldName)
                }
                MangleConstant.VALUE_PARAMETERS.prefix -> {
                    // function
                    while (idx < value.length && value[idx] != MangleConstant.VALUE_PARAMETERS.suffix) {
                        val type = parseType()
                        var isVararg = false
                        checkNotEnd()
                        if (value[idx] == '.') { // ellipsis
                            consume('.')
                            consume('.')
                            consume('.')
                            isVararg = true
                        }

                        valueParameters.add(ParsedSignature.FunctionSignature.ValueParameter(type, isVararg))
                        consumeIf(MangleConstant.VALUE_PARAMETERS.separator)
                    }

                    consume(MangleConstant.VALUE_PARAMETERS.suffix)
                }
                '|' -> {
                    // type parameter
                    assert(fileName == null) { value }
                    assert(extensionReceiver == null) { value }
                    checkNotEnd()
                    val num = Integer.parseInt(parseNumber())
                    checkIsOver()
                    return ParsedSignature.TypeParameterSignature(ParsedSignature.ClassSignature(pkgName, declarationName), num)
                }
            }

            if (!isProperty) {
                consume(MangleConstant.RETURN_TYPE_MARK)
                returnType = parseType()
            }

            assert(returnType != null) { value }

            val typeParameters = mutableListOf<ParsedSignature.TypeParameter>()

            if (consumeIf(MangleConstant.TYPE_PARAMETERS.prefix)) {
                do {
                    consumeIf(MangleConstant.TYPE_PARAMETERS.separator)
                    val variance = when (value[idx]) {
                        '+', '-' -> value[idx++].toString()
                        else -> ""
                    }
                    consume(MangleConstant.UPPER_BOUNDS.prefix)

                    val upperBounds = mutableListOf<ParsedSignature.Type>()
                    do {
                        consumeIf(MangleConstant.UPPER_BOUNDS.separator)
                        upperBounds.add(parseType())
                    } while (idx < value.length && value[idx] == MangleConstant.UPPER_BOUNDS.separator)

                    consume(MangleConstant.UPPER_BOUNDS.suffix)

                    typeParameters.add(ParsedSignature.TypeParameter(variance, upperBounds))
                } while (idx < value.length && value[idx] == MangleConstant.TYPE_PARAMETERS.separator)

                consume(MangleConstant.TYPE_PARAMETERS.suffix)
            }

            val isSuspend = consumeIf2('|', MangleConstant.SUSPEND_MARK)

            val containerSig = if (isProperty) {
                assert(valueParameters.isEmpty()) { value }
                ParsedSignature.PropertySignature(fileName, pkgName, declarationName, extensionReceiver, typeParameters, returnType!!)
            } else {
                ParsedSignature.FunctionSignature(
                    fileName,
                    pkgName,
                    declarationName,
                    extensionReceiver,
                    valueParameters,
                    typeParameters,
                    returnType!!,
                    isSuspend
                )
            }

            if (consumeIf(MangleConstant.FIELD_MARK)) {
                assert(isProperty)
                checkIsOver()
                return ParsedSignature.FieldSignature(containerSig, null) // backing fields
            }


            if (consumeIf('|')) {
                val index = parseNumber()
                consumeIf('S')
                checkIsOver()

                return ParsedSignature.TypeParameterSignature(containerSig, Integer.parseInt(index))
            }

            checkIsOver()
            return containerSig
        }

    }

    private fun parseSignature(): ParsedSignature {
        return StringSignatureParser(value).parseSignature()
    }

    sealed class ParsedSignature {


        protected abstract fun asStringTo(sb: StringBuilder)
        fun asString(): String = buildString { asStringTo(this) }

        abstract fun containerSignature(): ParsedSignature

        abstract class TopLevelSignature : ParsedSignature() {
            abstract val packageFqName: FqName
            abstract val declarationFqn: FqName
            abstract val fileName: String?

            override fun asStringTo(sb: StringBuilder) {
                fileName?.let {
                    sb.append(it)
                    sb.append('/')
                }
                sb.append(packageFqName.asString())
                sb.append('/')
                sb.append(declarationFqn)
            }
        }

//        abstract class LocalSignature(val container: ParsedSignature, val id: Int) : ParsedSignature() {
//
//        }

        class LocalMemberSignature(val classId: Int, val signatureString: String) : ParsedSignature() {

            override fun asStringTo(sb: StringBuilder) {
                TODO("Not yet implemented")
            }

            override fun containerSignature(): ParsedSignature {
                TODO("Not yet implemented")
            }
        }

        class ClassSignature(override val packageFqName: FqName, private val classFqName: FqName) : TopLevelSignature() {

            override fun containerSignature(): ParsedSignature {
                val parent = classFqName.parent()
                return if (parent.isRoot) this else ClassSignature(packageFqName, parent)
            }

            constructor(pkg: String, cls: String) : this(FqName(pkg), FqName(cls))
            override val declarationFqn: FqName
                get() = classFqName
            override val fileName: String?
                get() = null
        }

        class PropertySignature(
            override val fileName: String?,
            override val packageFqName: FqName,
            override val declarationFqn: FqName,
            val extensionType: Type?,
            val typeParameters: List<TypeParameter>,
            val returnType: Type
        ) : TopLevelSignature() {
            constructor(
                _fileName: String?,
                _packageFqName: String,
                _declarationFqn: String,
                _extensionType: Type?,
                _typeParameters: List<TypeParameter>,
                _returnType: Type
            ) : this(
                _fileName,
                FqName(_packageFqName),
                FqName(_declarationFqn),
                _extensionType,
                _typeParameters,
                _returnType
            )

            override fun containerSignature(): ParsedSignature {
                val parent = declarationFqn.parent()
                return if (parent.isRoot) this else ClassSignature(packageFqName, parent)
            }

            override fun asStringTo(sb: StringBuilder) {
                super.asStringTo(sb)
                extensionType?.let {
                    sb.append(MangleConstant.EXTENSION_RECEIVER_MARK)
                    it.asString(sb)
//                    sb.append(it)
                    sb.append(MangleConstant.EXTENSION_RECEIVER_MARK)
                }

                sb.append(MangleConstant.RETURN_TYPE_MARK)
//                sb.append(returnType)
                returnType.asString(sb)

                if (typeParameters.isNotEmpty()) {
                    typeParameters.collectForMangler(sb, MangleConstant.TYPE_PARAMETERS) {
                        it.asString(this)
                    }
                }
            }
        }

        sealed class Type {

            abstract fun asString(sb: StringBuilder)

            object DynamicType : Type() {
                override fun asString(sb: StringBuilder) {
                    sb.append("^d")
                }
            }

            object ErrorType : Type() {
                override fun asString(sb: StringBuilder) {
                    sb.append("^e")
                }
            }

            sealed class TypeArgument {

                abstract fun asString(sb: StringBuilder)

                object Star : TypeArgument() {
                    override fun asString(sb: StringBuilder) {
                        sb.append('*')
                    }
                }

                class Arg(val variance: String, val type: Type) : TypeArgument() {
                    override fun asString(sb: StringBuilder) {
                        sb.append(variance)
                        type.asString(sb)
                    }
                }
            }

            class SimpleType(val classifier: String, val arguments: List<TypeArgument>, val nullability: String) : Type() {

                override fun asString(sb: StringBuilder) {
                    sb.append(classifier)
                    if (arguments.isNotEmpty()) {
                        arguments.collectForMangler(sb, MangleConstant.TYPE_ARGUMENTS) {
                            it.asString(sb)
                        }
                    }
                    sb.append(nullability)
                }
            }
        }

        class TypeParameter(val variance: String, val bounds: List<Type>) {
            fun asString(sb: StringBuilder) {
                sb.append(variance)
                bounds.collectForMangler(sb, MangleConstant.UPPER_BOUNDS) { it.asString(sb) }
            }
        }


        class TypeParameterSignature(val containerSig: TopLevelSignature, val idx: Int) : TopLevelSignature() {
            override val declarationFqn: FqName
                get() = containerSig.declarationFqn
            override val packageFqName: FqName
                get() = containerSig.packageFqName
            override val fileName: String?
                get() = containerSig.fileName

            override fun containerSignature(): ParsedSignature = containerSig

            override fun asStringTo(sb: StringBuilder) {
                super.asStringTo(sb)
                sb.append('|')
                sb.append(idx)
            }
        }

        class FunctionSignature(
            override val fileName: String?,
            override val packageFqName: FqName,
            val classFqName: FqName,
            val extensionType: Type?,
            val valueParameters: List<ValueParameter>,
            val typeParameters: List<TypeParameter>,
            val returnType: Type,
            val isSuspend: Boolean
        ) : TopLevelSignature() {

            private fun constructPropertyFromGetter(propertyFqName: FqName): ParsedSignature {
                return PropertySignature(fileName, packageFqName, propertyFqName, extensionType, typeParameters, returnType)
            }

            private fun constructPropertyFromSetter(propertyFqName: FqName): ParsedSignature {
                val value_Param = valueParameters.single()
                return PropertySignature(fileName, packageFqName, propertyFqName, extensionType, typeParameters, value_Param.type)
            }

            override fun asStringTo(sb: StringBuilder) {
                super.asStringTo(sb)

                extensionType?.let {
                    sb.append(MangleConstant.EXTENSION_RECEIVER_MARK)
                    it.asString(sb)
//                    sb.append(it)
                    sb.append(MangleConstant.EXTENSION_RECEIVER_MARK)
                }

                valueParameters.collectForMangler(sb, MangleConstant.VALUE_PARAMETERS) {
                    it.asString(this)
                }

                sb.append(MangleConstant.RETURN_TYPE_MARK)
                returnType.asString(sb)
//                sb.append(returnType)

                if (typeParameters.isNotEmpty()) {
                    typeParameters.collectForMangler(sb, MangleConstant.TYPE_PARAMETERS) {
                        it.asString(this)
                    }
                }

                if (isSuspend) {
                    sb.append('|')
                    sb.append(MangleConstant.SUSPEND_MARK)
                }
            }

            override fun containerSignature(): ParsedSignature {

                val classFqn = classFqName.asString()
                if (classFqn.last() == '>') {

                    val openIdx = classFqn.indexOf('<')

                    val shortName = classFqn.substring(openIdx)

                    val parentName = classFqn.substring(0, openIdx - 1)

                    val parentFqn = FqName(parentName)

                    return when (shortName[1]) {
                        'i' -> ClassSignature(packageFqName, parentFqn)
                        'g' -> constructPropertyFromGetter(parentFqn)
                        's' -> constructPropertyFromSetter(parentFqn)
                        else -> error("Unknown extra name $shortName")
                    }
                }

                if (fileName != null) return this

                val parent = classFqName.parent()
                return if (parent.isRoot) this
                else ClassSignature(packageFqName, parent)
            }

            constructor(
                _fileName: String?,
                _packageFqName: String,
                _classFqName: String,
                _extensionType: Type?,
                _valueParameters: List<ValueParameter>,
                _typeParameters: List<TypeParameter>,
                _returnType: Type,
                _isSuspend: Boolean
            ) : this(
                _fileName,
                FqName(_packageFqName),
                FqName(_classFqName),
                _extensionType,
                _valueParameters,
                _typeParameters,
                _returnType,
                _isSuspend
            )

            class ValueParameter(val type: Type, val isVararg: Boolean) {
                fun asString(sb: StringBuilder) {
                    type.asString(sb)
                    if (isVararg) sb.append(MangleConstant.VAR_ARG_MARK)
                }
            }

            override val declarationFqn: FqName
                get() = classFqName
        }

        class FieldSignature(val containerSig: TopLevelSignature, val fieldName: String?) : TopLevelSignature() {
            override val declarationFqn: FqName
                get() = containerSig.declarationFqn
            override val packageFqName: FqName
                get() = containerSig.packageFqName
            override val fileName: String?
                get() = containerSig.fileName

            override fun containerSignature(): ParsedSignature = containerSig

            override fun asStringTo(sb: StringBuilder) {
                super.asStringTo(sb)
                sb.append(MangleConstant.FIELD_MARK)
                fieldName?.let { sb.append(it) }
            }
        }

//        class LocalMethodSignature() : LocalSignature() {
//
//        }
//
//        class LocalPropertySignature() : LocalSignature() {
//
//        }
    }

    override fun toString(): String {
        return "Signature[$value]"
    }
}

val StringSignature.isPubliclyVisible: Boolean get() = !isLocal

val StringSignature.isKotlinPackage: Boolean get() = isPubliclyVisible && value.startsWith("kotlin/")

interface StringSignatureComposer {
    fun composeSignature(descriptor: DeclarationDescriptor): StringSignature?
    fun composeEnumEntrySignature(descriptor: ClassDescriptor): StringSignature?
    fun composeFieldSignature(descriptor: PropertyDescriptor): StringSignature?
    fun composeAnonInitSignature(descriptor: ClassDescriptor): StringSignature?
}