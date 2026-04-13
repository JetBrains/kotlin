package testing.groovytest

class Test {
    fun foo(a: Any) = this
    val test: Test = this
    fun test() = this
    fun get(index: Int) = this

    fun i() = 0
    var i: Int = 0
}

fun bar() = Test()

fun test() {
    val t = Test()
    val s = t

    /*false*/t.test()
    /*false*/t.test()!!
    /*false*/t.test()?.test
    /*false*/t.test()[0]
    /*false*/t.test[0]
    /*false*/t.test[t.i()]!!

    /*false*/t.test
    /*false*/t!!.test
    /*false*/t?.test

    t[/*false*/s.i()]
    t[/*false*/s!!.i()]
    t[/*false*/s?.i()]
    t[/*false*/s[0]?.i()]

    /*false*/t[0].test
    /*false*/t[0]!!.test
    /*false*/t[0]?.test

    bar()./*true*/test
    bar()!!./*true*/test
    bar()?./*true*/test

    t[0]./*true*/test()
    t[0]!!./*true*/test()
    t[0]?./*true*/test()

    t.foo(/*false*/t.test)
    t.foo(/*false*/t!!.test)
    t.foo(/*false*/t?.test)
    t.foo(/*false*/t[0].test)

    t.foo(t./*true*/test())
    t.foo(t!!./*true*/test)
    t.foo(t?./*true*/test())
    t.foo(t[0]./*true*/test)

    /*false*/t.i++
    /*false*/t!!.i++
    /*false*/t?.i++
    /*false*/t[0].i++
    /*false*/t.test.i++

    t./*true*/i++
    t!!./*true*/i++
    t?./*true*/i++
    t[0]./*true*/i++
    t.test./*true*/i++

    val t1 = 1
    val t2 = /*false*/t1

    /*false*/t1 + /*false*/t2
}