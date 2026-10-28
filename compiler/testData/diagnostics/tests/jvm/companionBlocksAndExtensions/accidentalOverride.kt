// RUN_PIPELINE_TILL: CODEGEN
// LANGUAGE: +CompanionBlocks

@file:OptIn(kotlin.ExperimentalVersionOverloading::class)

@JvmInline value class InlineString(val value: String)

open class A {
    val prop: Int = 0
    val prop2: String = ""
    fun memberFoo(x: List<Int>) {}
    fun memberBar(x: List<Int>) {}
    fun memberBaz(x: List<Int>) {}
    fun memberBaz2(x: List<Int>) {}
    fun memberBaz3(x: List<Int>) {}
    fun memberBaz4(x: List<Int>) {}

    companion {
        val prop3 get() = ""

        fun compFoo(x: List<Int>) {}
        fun compBar(x: List<Int>) {}
        fun compBaz(x: List<Int>) {}
        fun compBaz2(x: List<Int>) {}
        fun compBaz3(x: List<Int>) {}
        fun compBaz4(x: List<Int>) {}

        fun compWithInlineClass(s: InlineString) {}
    }
}

class B : A() {
    companion {
        <!ACCIDENTAL_OVERRIDE!>val prop: Int<!> = 0
        <!ACCIDENTAL_OVERRIDE!>val prop2: String<!> = ""

        <!ACCIDENTAL_OVERRIDE!>fun memberFoo(x: List<Int>)<!> {}
        <!ACCIDENTAL_OVERRIDE!>fun memberBar(x: List<String>)<!> {}
        @JvmOverloads
        <!ACCIDENTAL_OVERRIDE!>fun memberBaz(x: List<Int>, y: Int = 0) {}<!>
        @JvmOverloads
        <!ACCIDENTAL_OVERRIDE!>fun memberBaz2(x: List<String>, y: Int = 0) {}<!>
        <!ACCIDENTAL_OVERRIDE!>fun memberBaz3(x: List<Int>, @IntroducedAt("1.1") y: Int = 0) {}<!>
        <!ACCIDENTAL_OVERRIDE!>fun memberBaz4(x: List<String>, @IntroducedAt("1.1") y: Int = 0) {}<!>

        fun compFoo(x: List<Int>) {}
        <!ACCIDENTAL_OVERRIDE!>fun compBar(x: List<String>)<!> {}
        @JvmOverloads
        <!ACCIDENTAL_OVERRIDE!>fun compBaz(x: List<Int>, y: Int = 0) {}<!>
        @JvmOverloads
        <!ACCIDENTAL_OVERRIDE!>fun compBaz2(x: List<String>, y: Int = 0) {}<!>
        <!ACCIDENTAL_OVERRIDE!>fun compBaz3(x: List<Int>, @IntroducedAt("1.1") y: Int = 0) {}<!>
        <!ACCIDENTAL_OVERRIDE!>fun compBaz4(x: List<String>, @IntroducedAt("1.1") y: Int = 0) {}<!>

        // KT-90607
        fun compWithInlineClass(s: InlineString) {}
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration */
