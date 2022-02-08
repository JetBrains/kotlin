/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.util

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.name.FqName

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

    val declarationFqName: String
        get() = topLevelPrefix().third

    val shortName: String get() = declarationFqName.substringAfterLast('.')

    fun packageFqName(): FqName = FqName(topLevelPrefix().second)

    private fun topLevelPrefix() = StringSignatureParser(value).parseTopLevelPrefix()

    fun localIndex(): Int {
        assert(isLocal)
        return Integer.parseInt(value.substring(1))
    }


    class StringSignatureParser(private val value: String) {
        private var idx = 0

        private fun Char.isValidId(): Boolean {
            return this == '_' || this.isLetterOrDigit()
        }

        private fun Char.isValidIdStart(): Boolean {
            return this == '_' || this.isLetter()
        }

        private fun parseIdentificator(): String {
            return buildString {
                do {
                    if (consumeIf2('$', '$')) {
                        append("\$\$")
                    }
                    append(value[idx++])
                } while (idx < value.length && value[idx].isValidId())
            }
        }

        private fun parseNumber(): String {
            return buildString {
                while (idx < value.length && value[idx].isDigit()) {
                    append(value[idx++])
                }
            }
        }

        private fun parseType(): String {
            if (consumeIf2('^', 'd')) return MangleConstant.DYNAMIC_TYPE_MARK
            if (consumeIf2('^', 'e')) return MangleConstant.ERROR_TYPE_MARK
            if (consumeIf('^')) error("Unknown type kind \"^${value[idx]}\" in $value")

            return buildString {
                var isTypeParameter = false
                if (value[idx] == '{') {
                    val idx2 = value.indexOf('}', idx)
                    assert(idx2 > 0)
                    assert(idx2 > idx + 3) { value }
                    append(value.substring(idx, idx2 + 1))
                    idx = idx2 + 1
                    isTypeParameter = true
                } else {
                    val (pkg, cls) = parseClassId()
                    append(pkg)
                    append('/')
                    append(cls)
                }

                if (consumeIf(MangleConstant.TYPE_ARGUMENTS.prefix)) {
                    assert(!isTypeParameter)

                    append(MangleConstant.TYPE_ARGUMENTS.prefix)

                    do {
                        if (consumeIf(MangleConstant.TYPE_ARGUMENTS.separator)) append(MangleConstant.TYPE_ARGUMENTS.separator)
                        if (consumeIf(MangleConstant.STAR_MARK)) {
                            append(MangleConstant.STAR_MARK)
                        } else {
                            if (value[idx] == '+' || value[idx] == '-') append(value[idx++])
                            append(parseType())
                        }
                    } while (idx < value.length && value[idx] == MangleConstant.TYPE_ARGUMENTS.separator)

                    consume(MangleConstant.TYPE_ARGUMENTS.suffix)
                    append(MangleConstant.TYPE_ARGUMENTS.suffix)
                }

                var isNullable = false
                if (consumeIf('?')) {
                    append('?')
                    isNullable = true
                }
            }
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
            if (value[idx++] != c) error("Expecting '$c' at position #$idx in \"$value\"")
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

            val extraName = if (consumeIf('^')) {
                checkNotEnd()
                buildString {
                    append(value[idx++])
                }.also {
                    checkNotEnd()
                }
            } else null

            val extensionReceiver = if (consumeIf(MangleConstant.EXTENSION_RECEIVER_MARK)) {
                parseType().also {
                    consume(MangleConstant.EXTENSION_RECEIVER_MARK)
                }
            } else null

            checkNotEnd()

            var returnType = ""
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
                    assert(extraName == null) { value }
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
                    assert(extraName == null) { value }
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

            assert(returnType.isNotEmpty()) { value }

            val typeParameters = mutableListOf<ParsedSignature.TypeParameter>()

            if (consumeIf(MangleConstant.TYPE_PARAMETERS.prefix)) {
                do {
                    consumeIf(MangleConstant.TYPE_PARAMETERS.separator)
                    val variance = when (value[idx]) {
                        '+', '-' -> value[idx++].toString()
                        else -> ""
                    }
                    consume(MangleConstant.UPPER_BOUNDS.prefix)

                    val upperBounds = mutableListOf<String>()
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
                assert(extraName == null) { value }
                assert(valueParameters.isEmpty()) { value }
                ParsedSignature.PropertySignature(fileName, pkgName, declarationName, extensionReceiver, typeParameters, returnType)
            } else {
                ParsedSignature.FunctionSignature(
                    fileName,
                    pkgName,
                    declarationName,
                    extraName,
                    extensionReceiver,
                    valueParameters,
                    typeParameters,
                    returnType,
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
            val extensionType: String?,
            val typeParameters: List<TypeParameter>,
            val returnType: String
        ) : TopLevelSignature() {
            constructor(
                _fileName: String?,
                _packageFqName: String,
                _declarationFqn: String,
                _extensionType: String?,
                _typeParameters: List<TypeParameter>,
                _returnType: String
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
                    sb.append(it)
                    sb.append(MangleConstant.EXTENSION_RECEIVER_MARK)
                }

                sb.append(MangleConstant.RETURN_TYPE_MARK)
                sb.append(returnType)

                if (typeParameters.isNotEmpty()) {
                    typeParameters.collectForMangler(sb, MangleConstant.TYPE_PARAMETERS) {
                        it.asString(this)
                    }
                }
            }
        }

        class TypeParameter(val variance: String, val bounds: List<String>) {
            fun asString(sb: StringBuilder) {
                sb.append(variance)
                bounds.collectForMangler(sb, MangleConstant.UPPER_BOUNDS) { append(it) }
            }
        }
//        class TypeArgument(val variance: String, val )

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
            val extraName: String?,
            val extensionType: String?,
            val valueParameters: List<ValueParameter>,
            val typeParameters: List<TypeParameter>,
            val returnType: String,
            val isSuspend: Boolean
        ) : TopLevelSignature() {

            private fun constructPropertyFromGetter(): ParsedSignature {
                return PropertySignature(fileName, packageFqName, declarationFqn, extensionType, typeParameters, returnType)
            }

            private fun constructPropertyFromSetter(): ParsedSignature {
                val value_Param = valueParameters.single()
                return PropertySignature(fileName, packageFqName, declarationFqn, extensionType, typeParameters, value_Param.type)
            }

            override fun asStringTo(sb: StringBuilder) {
                super.asStringTo(sb)

                extraName?.let {
                    sb.append('^')
                    sb.append(it)
                }

                extensionType?.let {
                    sb.append(MangleConstant.EXTENSION_RECEIVER_MARK)
                    sb.append(it)
                    sb.append(MangleConstant.EXTENSION_RECEIVER_MARK)
                }

                valueParameters.collectForMangler(sb, MangleConstant.VALUE_PARAMETERS) {
                    it.asString(this)
                }

                sb.append(MangleConstant.RETURN_TYPE_MARK)
                sb.append(returnType)

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

                extraName?.let { extra ->
                    if (extra == "c") return ClassSignature(packageFqName, classFqName)
                    if (extra == "g") return constructPropertyFromGetter()
                    if (extra == "s") return constructPropertyFromSetter()
                    error("Unknown extra name '$extra'")
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
                _extraName: String?,
                _extensionType: String?,
                _valueParameters: List<ValueParameter>,
                _typeParameters: List<TypeParameter>,
                _returnType: String,
                _isSuspend: Boolean
            ) : this(
                _fileName,
                FqName(_packageFqName),
                FqName(_classFqName),
                _extraName,
                _extensionType,
                _valueParameters,
                _typeParameters,
                _returnType,
                _isSuspend
            )

            class ValueParameter(val type: String, val isVararg: Boolean) {
                fun asString(sb: StringBuilder) {
                    sb.append(type)
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