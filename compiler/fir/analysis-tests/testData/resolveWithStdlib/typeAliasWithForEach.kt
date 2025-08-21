// RUN_PIPELINE_TILL: BACKEND
// FULL_JDK
import java.util.*

interface ArgsInfo

class ArgsInfoImpl : ArgsInfo {
    constructor(info: ArgsInfo) {}
}

typealias Arguments = Map<String, ArgsInfo>

fun Arguments.deepCopy(): Arguments {
    val result = HashMap<String, ArgsInfo>()
    this.forEach { key, value -> result[key] = ArgsInfoImpl(value) }
    return result
}

/* GENERATED_FIR_TAGS: assignment, classDeclaration, flexibleType, funWithExtensionReceiver, functionDeclaration,
inProjection, interfaceDeclaration, javaFunction, lambdaLiteral, localProperty, propertyDeclaration, samConversion,
secondaryConstructor, thisExpression, typeAliasDeclaration */
