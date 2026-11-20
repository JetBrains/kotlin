// DECLARATION_TYPE: org.jetbrains.kotlin.psi.KtClass
// COMPILER_ARGUMENTS: -Xmetadata-version=2.1.0
package one

annotation class Defaults(
    val ub: UByte = UByte.MAX_VALUE,
    val us: UShort = UShort.MAX_VALUE,
    val ui: UInt = UInt.MAX_VALUE,
    val ul: ULong = ULong.MAX_VALUE,
)
