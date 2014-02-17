trait A {
    fun foo()
}

trait B : A {
    fun bar()
}

trait C

trait D : A

fun test(a: A?) {
    if (a != null && a is B?) {
        <info descr="Automatically cast to B">a</info>.bar()
    }

    if (a is B && a is C) {
        <info descr="Automatically cast to B">a</info>.foo()
    }

    if (a is B? && a is C?) {
        <info descr="Automatically cast to B?">a</info><info>?.</info>bar()
    }

    a<info>?.</info>foo()
    if (a is B? && a is C?) {
        a<info>?.</info>foo()
    }

    if (a is B && a is D) {
        //when it's resolved, the message should be 'Automatically cast to A'
        a.<error>foo</error>
    }
}
