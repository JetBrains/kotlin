// RUN_PIPELINE_TILL: FRONTEND
// TARGET_BACKEND: JVM_IR

typealias ABoolean = Boolean
typealias AChar = Char
typealias AInt = Int
typealias ALong = Long
typealias AShort = Short
typealias AByte = Byte
typealias AFloat = Float
typealias ADouble = Double

fun main() {
    ABoolean(<!TOO_MANY_ARGUMENTS!>false<!>)
    Boolean(<!TOO_MANY_ARGUMENTS!>false<!>)

    AChar(<!TOO_MANY_ARGUMENTS!>'c'<!>)
    Char(<!TOO_MANY_ARGUMENTS!>'c'<!>)

    AInt(<!TOO_MANY_ARGUMENTS!>42<!>)
    Int(<!TOO_MANY_ARGUMENTS!>42<!>)

    ALong(<!TOO_MANY_ARGUMENTS!>42<!>)
    Long(<!TOO_MANY_ARGUMENTS!>42<!>)

    AShort(<!TOO_MANY_ARGUMENTS!>42<!>)
    Short(<!TOO_MANY_ARGUMENTS!>42<!>)

    AByte(<!TOO_MANY_ARGUMENTS!>42<!>)
    Byte(<!TOO_MANY_ARGUMENTS!>42<!>)

    AFloat(<!TOO_MANY_ARGUMENTS!>4.2f<!>)
    Float(<!TOO_MANY_ARGUMENTS!>4.2f<!>)

    ADouble(<!TOO_MANY_ARGUMENTS!>4.2<!>)
    Double(<!TOO_MANY_ARGUMENTS!>4.2<!>)
}

/* GENERATED_FIR_TAGS: functionDeclaration, integerLiteral, typeAliasDeclaration */
