/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kotlin.reflect.jvm.internal

import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.builtins.isNumberedFunctionClassFqName
import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMap
import org.jetbrains.kotlin.name.FqNameUnsafe
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.renderer.render
import org.jetbrains.kotlin.renderer.renderFlexibleMutabilityOrArrayElementVarianceType
import kotlin.reflect.*
import kotlin.reflect.full.contextParameters
import kotlin.reflect.full.extensionReceiverParameter
import kotlin.reflect.full.valueParameters
import kotlin.reflect.jvm.internal.types.AbstractKType
import kotlin.reflect.jvm.jvmName

internal object ReflectionObjectRenderer {
    private fun StringBuilder.appendReceiverType(receiver: KParameter): StringBuilder =
        append(renderType(receiver.type)).append(".")

    private fun StringBuilder.appendReceivers(callable: KCallable<*>) {
        val receivers = (callable as KCallableImpl<*>).receiverParameters.filter {
            it.kind == KParameter.Kind.INSTANCE || it.kind == KParameter.Kind.EXTENSION_RECEIVER
        }
        receivers.getOrNull(0)?.let { appendReceiverType(it) }
        receivers.getOrNull(1)?.let { append("(").appendReceiverType(it).append(")") }
    }

    @OptIn(ExperimentalContextParameters::class)
    private fun StringBuilder.appendContexts(callable: KCallable<*>) {
        val parameters = callable.contextParameters
        if (parameters.isEmpty()) return

        parameters.joinTo(this, prefix = "context(", postfix = ") ") { parameter ->
            (parameter.name ?: "_") + ": " + parameter.type
        }
    }

    private fun StringBuilder.appendName(name: String) {
        append(Name.identifier(name).render())
    }

    private fun renderCallable(callable: KCallable<*>): String {
        return when (callable) {
            is KProperty<*> -> renderProperty(callable)
            is KFunction<*> -> renderFunction(callable)
            else -> error("Illegal callable: $callable")
        }
    }

    // TODO: include visibility
    fun renderProperty(property: KProperty<*>): String {
        return buildString {
            appendContexts(property)
            append(if (property is KMutableProperty<*>) "var " else "val ")
            appendReceivers(property)
            appendName(property.name)

            append(": ")
            append(renderType(property.returnType))
        }
    }

    fun renderFunction(function: KFunction<*>): String {
        return buildString {
            appendContexts(function)
            append("fun ")
            appendReceivers(function)
            appendName(function.name)

            function.valueParameters.joinTo(this, separator = ", ", prefix = "(", postfix = ")") {
                renderType(it.type) // TODO: vararg
            }

            append(": ")
            append(renderType(function.returnType))
        }
    }

    fun renderLambda(lambda: KFunction<*>): String {
        return buildString {
            lambda.extensionReceiverParameter?.let {
                append(renderType(it.type))
                append(".")
            }

            lambda.valueParameters.joinTo(this, separator = ", ", prefix = "(", postfix = ")") {
                renderType(it.type)
            }

            append(" -> ")
            append(renderType(lambda.returnType))
        }
    }

    fun renderParameter(parameter: KParameter): String {
        return buildString {
            when (parameter.kind) {
                KParameter.Kind.INSTANCE -> append("instance parameter")
                @OptIn(ExperimentalContextParameters::class)
                KParameter.Kind.CONTEXT,
                    -> append("context parameter ${parameter.name}")
                KParameter.Kind.EXTENSION_RECEIVER -> append("extension receiver parameter")
                KParameter.Kind.VALUE -> append("parameter #${parameter.index} ${parameter.name}")
            }

            append(" of ")
            append(renderCallable((parameter as KParameterImpl).callable))
        }
    }

    fun renderType(type: KType, renderRawArgumentPrefix: Boolean = false): String {
        type as AbstractKType

        if (type.isRawType) {
            return renderType(type.lowerBoundIfFlexible()!!, renderRawArgumentPrefix = true)
        }

        val lowerBound = type.lowerBoundIfFlexible()
        val upperBound = type.upperBoundIfFlexible()
        if (lowerBound != null && upperBound != null) {
            return renderFlexibleType(renderType(lowerBound), renderType(upperBound))
        }

        return buildString {
            type.abbreviation?.let {
                append(it)
                append(" /* = ")
            }

            when (val classifier = type.classifier) {
                is KTypeParameter -> {
                    appendName(classifier.name)
                    if (type.isMarkedNullable) {
                        append("?")
                    } else if (type.isDefinitelyNotNullType) {
                        append(" & Any")
                    }
                }
                is KClass<*> -> {
                    val fqName = getTypeClassFqName(type, classifier) ?: FqNameUnsafe(classifier.jvmName)
                    if (isNumberedFunctionClassFqName(fqName) && KTypeProjection.STAR !in type.arguments) {
                        renderFunctionType(type)
                    } else {
                        renderSimpleType(classifier, fqName, type.arguments, type.isMarkedNullable, renderRawArgumentPrefix)
                    }
                }
                is KTypeAliasImpl -> {
                    classifier.fqName.pathSegments().joinTo(this, separator = ".") { it.render() }
                    renderTypeArgumentsAndNullability(type.arguments, type.isMarkedNullable, renderRawArgumentPrefix)
                }
                else -> {
                    append("???")
                }
            }

            if (type.abbreviation != null) {
                append(" */")
            }
        }
    }

    private fun getTypeClassFqName(type: AbstractKType, klass: KClass<*>): FqNameUnsafe? {
        if (type.isNothingType)
            return StandardNames.FqNames.nothing
        val fqName = klass.qualifiedName?.let(::FqNameUnsafe) ?: return null
        if (type.isMutableCollectionType)
            return JavaToKotlinClassMap.readOnlyToMutable(fqName)?.toUnsafe()
        return fqName
    }

    private fun StringBuilder.renderFunctionType(type: AbstractKType) {
        if (type.isMarkedNullable) append("(")
        if (type.isSuspendFunctionType) append("suspend ")
        type.arguments.dropLast(1).joinTo(this, prefix = "(", postfix = ") -> ")
        append(type.arguments.last())
        if (type.isMarkedNullable) append(")?")
    }

    private fun StringBuilder.renderSimpleType(
        klass: KClass<*>,
        classFqName: FqNameUnsafe,
        allArguments: List<KTypeProjection>,
        isMarkedNullable: Boolean,
        renderRawArgumentPrefix: Boolean,
    ) {
        if (klass.typeParameters.size < allArguments.size && klass.java.declaringClass != null) {
            renderSimpleType(
                klass.java.declaringClass.kotlin,
                classFqName.parent(),
                allArguments.drop(klass.typeParameters.size),
                false,
                renderRawArgumentPrefix
            )
            append(".")
            append(classFqName.shortName().render())
        } else {
            append(classFqName.render())
        }

        renderTypeArgumentsAndNullability(allArguments.take(klass.typeParameters.size), isMarkedNullable, renderRawArgumentPrefix)
    }

    private fun StringBuilder.renderTypeArgumentsAndNullability(
        typeArguments: List<KTypeProjection>, isMarkedNullable: Boolean, renderRawArgumentPrefix: Boolean,
    ) {
        if (typeArguments.isNotEmpty()) {
            typeArguments.joinTo(this, prefix = "<", postfix = ">") {
                (if (renderRawArgumentPrefix) "(raw) " else "") + it.toString()
            }
        }
        if (isMarkedNullable) {
            append("?")
        }
    }

    private fun renderFlexibleType(lowerRendered: String, upperRendered: String): String {
        when {
            lowerRendered == upperRendered.replace("?", "") -> return upperRendered.replace("?", "!")
            upperRendered.endsWith("?") && ("$lowerRendered?") == upperRendered -> return "$lowerRendered!"
            "($lowerRendered)?" == upperRendered -> return "($lowerRendered)!"
        }

        return renderFlexibleMutabilityOrArrayElementVarianceType(
            lowerRendered,
            upperRendered,
            { (StandardNames.COLLECTIONS_PACKAGE_FQ_NAME.asString() + ".").takeIf { lowerRendered.startsWith(it) } ?: "" },
            { (StandardNames.BUILT_INS_PACKAGE_FQ_NAME.asString() + ".").takeIf { lowerRendered.startsWith(it) } ?: "" },
        ) ?: "($lowerRendered..$upperRendered)"
    }
}
