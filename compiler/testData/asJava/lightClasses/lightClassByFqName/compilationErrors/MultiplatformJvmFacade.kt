// mypack.MyFacadeKt
// SKIP_IDE_TEST
// MODULE: common
// FILE: commonFile1.kt
@file:JvmName("MyFacadeKt")
@file:JvmMultifileClass
package mypack

expect fun commonFunctionWithActualization(commonActualization: MyCommonClassWithActualization, common: MyCommonClass)
expect var commonVariableWithActualization: MyCommonClassWithActualization

fun commonFunction1(commonActualization: MyCommonClassWithActualization, common: MyCommonClass) {

}

expect class MyCommonClassWithActualization
class MyCommonClass

var commonVariable1: MyCommonClassWithActualization

// FILE: commonFile2.kt
@file:JvmName("MyFacadeKt")
@file:JvmMultifileClass
package mypack

fun commonFunction2(commonActualization: MyCommonClassWithActualization, common: MyCommonClass) {

}

var commonVariable2: MyCommonClassWithActualization

// MODULE: intermediate-common()()(common)
// FILE: intermediateFile1.kt
@file:JvmName("MyFacadeKt")
@file:JvmMultifileClass
package mypack

expect fun intermediateFunctionWithActualization(
    commonActualization: MyCommonClassWithActualization,
    intermediateActualization: IntermediateClassWithActualization,
    common: MyCommonClass,
    intermediate: MyIntermediateClass,
)

expect var intermediateVariableWithActualization: IntermediateClassWithActualization

fun intermediateFunction1(
    commonActualization: MyCommonClassWithActualization,
    intermediateActualization: IntermediateClassWithActualization,
    common: MyCommonClass,
    intermediate: MyIntermediateClass,
) {

}

expect class IntermediateClassWithActualization
class MyIntermediateClass

actual typealias MyCommonClassWithActualization = IntermediateClassWithActualization

var intermediateVariable1: IntermediateClassWithActualization

// FILE: intermediateFile2.kt
@file:JvmName("MyFacadeKt")
@file:JvmMultifileClass
package mypack

fun intermediateFunction2(
    commonActualization: MyCommonClassWithActualization,
    intermediateActualization: IntermediateClassWithActualization,
    common: MyCommonClass,
    intermediate: MyIntermediateClass,
) {

}

var intermediateVariable2: IntermediateClassWithActualization

// MODULE: main-jvm()()(intermediate-common)
// FILE: jvmFile1.kt
@file:JvmName("MyFacadeKt")
@file:JvmMultifileClass
package mypack

fun jvmFunction1(
    commonActualization: MyCommonClassWithActualization,
    intermediateActualization: IntermediateClassWithActualization,
    common: MyCommonClass,
    intermediate: MyIntermediateClass,
    jvm: MyJvmClass,
) {

}

var jvmVariable1: MyJvmClass

actual fun intermediateFunctionWithActualization(
    commonActualization: MyCommonClassWithActualization,
    intermediateActualization: IntermediateClassWithActualization,
    common: MyCommonClass,
    intermediate: MyIntermediateClass,
) {

}

actual var intermediateVariableWithActualization: IntermediateClassWithActualization

// FILE: jvmFile2.kt
@file:JvmName("MyFacadeKt")
@file:JvmMultifileClass
package mypack

actual fun commonFunctionWithActualization(
    commonActualization: MyCommonClassWithActualization,
    intermediateActualization: IntermediateClassWithActualization,
    common: MyCommonClass,
    intermediate: MyIntermediateClass,
) {

}

actual var commonVariableWithActualization: MyCommonClassWithActualization = "str"

fun jvmFunction2(
    commonActualization: MyCommonClassWithActualization,
    intermediateActualization: IntermediateClassWithActualization,
    common: MyCommonClass,
    intermediate: MyIntermediateClass,
    jvm: MyJvmClass,
) {

}

actual typealias IntermediateClassWithActualization = MyJvmClass

var jvmVariable2: MyJvmClass

class MyJvmClass
