
protocol interface Master {
    fun foo(x: String) = <!UNSUPPORTED!>x<!>
    fun bam(x: String = <!UNSUPPORTED!>"KOKOKO"<!>)
}

