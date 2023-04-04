// FIR_IDENTICAL
// ISSUE: KT-57198
// FILE: CustomGdbServerRunConfiguration.java

public class CustomGdbServerRunConfiguration implements CidrRunConfiguration {}

// FILE: main.kt
interface CidrBuildTarget<BC>
interface CidrRunConfiguration<BC, TARGET: CidrBuildTarget<BC>>

fun applyEditorTo(arg: CidrRunConfiguration<Any?, CidrBuildTarget<Any?>>) {}

fun main() {
    // Previously, for CidrRunConfiguration raw type, it's lower bound was resolved as CidrRunConfiguration<Any?, CidrBuildTarget<*>> in K2
    // That is not a subtype of CidrRunConfiguration<Any?, CidrBuildTarget<Any?>>
    applyEditorTo(<!ARGUMENT_TYPE_MISMATCH!>CustomGdbServerRunConfiguration()<!>) // K1: ok, K2: was ARGUMENT_TYPE_MISMATCH
}
