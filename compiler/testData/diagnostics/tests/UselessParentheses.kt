trait Foo {
    fun inc() : Foo
    fun dec() : Foo
    fun plus() : Foo
    fun plus(x : Any) : Foo
    fun minus(x : Any) : Foo
    fun times(x : Any) : Foo
    fun contains(x : Any) : Boolean
    fun rangeTo(x : Any) : Foo
    fun compareTo(x : Any) : Int
    fun get(x : Any) : Foo
}

fun foo(_x: Foo) : Any {
    var x = _x

    <!USELESS_PARENTHESES!>(x)<!>
    <!USELESS_PARENTHESES!>(x)<!> + x
    <!USELESS_PARENTHESES!>(x + x)<!>

    (x++).inc()
    +<!USELESS_PARENTHESES!>(x++)<!>
    <!USELESS_PARENTHESES!>(x++)<!> <!USELESS_CAST!>as<!> Foo
    <!USELESS_PARENTHESES!>(--x)<!> <!USELESS_CAST!>as<!> Foo
    <!USELESS_PARENTHESES!>(x++)<!> * <!USELESS_PARENTHESES!>(--x)<!>
    <!USELESS_PARENTHESES!>(x++)<!> + <!USELESS_PARENTHESES!>(--x)<!>
    <!USELESS_PARENTHESES!>(x++)<!>..<!USELESS_PARENTHESES!>(--x)<!>
    <!USELESS_PARENTHESES!>(x++)<!> plus <!USELESS_PARENTHESES!>(--x)<!>
    <!USELESS_PARENTHESES, USELESS_ELVIS!>(x++)<!> ?: <!USELESS_PARENTHESES!>(--x)<!>
    <!USELESS_PARENTHESES!>(x++)<!> in <!USELESS_PARENTHESES!>(--x)<!>
    <!USELESS_PARENTHESES!>(x++)<!> < <!USELESS_PARENTHESES!>(--x)<!>
    <!USELESS_PARENTHESES!>(x++)<!> == <!USELESS_PARENTHESES!>(--x)<!>
    x = <!USELESS_PARENTHESES!>(<!UNUSED_CHANGED_VALUE!>x++<!>)<!>
    x = <!USELESS_PARENTHESES!>(--x)<!>

    (x <!USELESS_CAST!>as<!> Foo).inc()
    +(x <!USELESS_CAST!>as<!> Foo)
    (x <!USELESS_CAST!>as<!> Foo) : Foo
    <!USELESS_PARENTHESES!>(x <!USELESS_CAST!>as<!> Foo)<!> * x
    <!USELESS_PARENTHESES!>(x <!USELESS_CAST!>as<!> Foo)<!> + x
    <!USELESS_PARENTHESES!>(x <!USELESS_CAST!>as<!> Foo)<!>..x
    <!USELESS_PARENTHESES!>(x <!USELESS_CAST!>as<!> Foo)<!> plus x
    <!USELESS_PARENTHESES, USELESS_ELVIS!>(x <!USELESS_CAST!>as<!> Foo)<!> ?: x
    <!USELESS_PARENTHESES!>(x <!USELESS_CAST!>as<!> Foo)<!> in x
    <!USELESS_PARENTHESES!>(x <!USELESS_CAST!>as<!> Foo)<!> > x
    <!USELESS_PARENTHESES!>(x <!USELESS_CAST!>as<!> Foo)<!> == x
    x = <!USELESS_PARENTHESES!>(x <!USELESS_CAST!>as<!> Foo)<!>

    (x * x).inc()
    +(x * x)
    (x * x) : Foo
    (x * x) * x
    <!USELESS_PARENTHESES!>(x * x)<!> + x
    <!USELESS_PARENTHESES!>(x * x)<!>..x
    <!USELESS_PARENTHESES!>(x * x)<!> plus x
    <!USELESS_PARENTHESES, USELESS_ELVIS!>(x * x)<!> ?: x
    <!USELESS_PARENTHESES!>(x * x)<!> in x
    <!USELESS_PARENTHESES!>(x * x)<!> < x
    <!USELESS_PARENTHESES!>(x * x)<!> == x
    x = <!USELESS_PARENTHESES!>(x * x)<!>

    (x + x).inc()
    +(x + x)
    (x + x) : Foo
    (x + x) * x
    (x + x) + x
    <!USELESS_PARENTHESES!>(x + x)<!>..x
    <!USELESS_PARENTHESES!>(x + x)<!> plus x
    <!USELESS_PARENTHESES, USELESS_ELVIS!>(x + x)<!> ?: x
    <!USELESS_PARENTHESES!>(x + x)<!> in x
    <!USELESS_PARENTHESES!>(x + x)<!> < x
    <!USELESS_PARENTHESES!>(x + x)<!> == x
    x = <!USELESS_PARENTHESES!>(x + x)<!>

    (x..x).inc()
    +(x..x)
    (x..x) : Foo
    (x..x) * x
    (x..x) + x
    (x..x)..x
    <!USELESS_PARENTHESES!>(x..x)<!> plus x
    <!USELESS_PARENTHESES, USELESS_ELVIS!>(x..x)<!> ?: x
    <!USELESS_PARENTHESES!>(x..x)<!> in x
    <!USELESS_PARENTHESES!>(x..x)<!> < x
    <!USELESS_PARENTHESES!>(x..x)<!> == x
    x = <!USELESS_PARENTHESES!>(x..x)<!>

    (x plus x).inc()
    +(x plus x)
    (x plus x) : Foo
    (x plus x) * x
    (x plus x) + x
    (x plus x)..x
    (x plus x) plus x
    <!USELESS_PARENTHESES, USELESS_ELVIS!>(x plus x)<!> ?: x
    <!USELESS_PARENTHESES!>(x plus x)<!> in x
    <!USELESS_PARENTHESES!>(x plus x)<!> < x
    <!USELESS_PARENTHESES!>(x plus x)<!> == x
    x = <!USELESS_PARENTHESES!>(x plus x)<!>

    (<!USELESS_ELVIS!>x<!> ?: x).inc()
    +(<!USELESS_ELVIS!>x<!> ?: x)
    (<!USELESS_ELVIS!>x<!> ?: x) : Foo
    (<!USELESS_ELVIS!>x<!> ?: x) * x
    (<!USELESS_ELVIS!>x<!> ?: x) + x
    (<!USELESS_ELVIS!>x<!> ?: x)..x
    (<!USELESS_ELVIS!>x<!> ?: x) plus x
    <!USELESS_ELVIS!>(<!USELESS_ELVIS!>x<!> ?: x)<!> ?: x
    <!USELESS_PARENTHESES!>(<!USELESS_ELVIS!>x<!> ?: x)<!> in x
    <!USELESS_PARENTHESES!>(<!USELESS_ELVIS!>x<!> ?: x)<!> < x
    <!USELESS_PARENTHESES!>(<!USELESS_ELVIS!>x<!> ?: x)<!> == x
    x = <!USELESS_PARENTHESES!>(<!USELESS_ELVIS!>x<!> ?: x)<!>

    !(x in x)
    (x in x) : Boolean
    (x in x) equals true
    <!USELESS_ELVIS!>(x in x)<!> ?: x
    (x in x) in x
    <!USELESS_PARENTHESES!>(x in x)<!> == true
    <!USELESS_PARENTHESES!>(x in x)<!> && true
    <!USELESS_PARENTHESES!>(x in x)<!> || false
    var <!UNUSED_VARIABLE!>a<!> = <!USELESS_PARENTHESES!>(x in x)<!>

    !(x < x)
    (x < x) : Boolean
    (x < x) equals true
    <!USELESS_ELVIS!>(x < x)<!> ?: x
    (x < x) in x
    <!USELESS_PARENTHESES!>(x < x)<!> == true
    <!USELESS_PARENTHESES!>(x < x)<!> && true
    <!USELESS_PARENTHESES!>(x < x)<!> || false
    var <!UNUSED_VARIABLE!>b<!> = <!USELESS_PARENTHESES!>(x < x)<!>

    !(x == x)
    (x == x) : Boolean
    (x == x) equals true
    <!USELESS_ELVIS!>(x == x)<!> ?: x
    (x == x) in x
    (x == x) == true
    <!USELESS_PARENTHESES!>(x == x)<!> && true
    <!USELESS_PARENTHESES!>(x == x)<!> || false
    var <!UNUSED_VARIABLE!>c<!> = <!USELESS_PARENTHESES!>(x == x)<!>

    !(true && false)
    (true && false) : Boolean
    (true && false) equals true
    <!USELESS_ELVIS!>(true && false)<!> ?: x
    (true && false) in x
    (true && false) == true
    <!USELESS_PARENTHESES!>(true && false)<!> && true
    <!USELESS_PARENTHESES!>(true && false)<!> || false
    var <!UNUSED_VARIABLE!>d<!> = <!USELESS_PARENTHESES!>(true && false)<!>

    !(true || false)
    (true || false) : Boolean
    (true || false) equals true
    <!USELESS_ELVIS!>(true || false)<!> ?: x
    (true || false) in x
    (true || false) == true
    (true || false) && true
    <!USELESS_PARENTHESES!>(true || false)<!> || false
    var <!UNUSED_VARIABLE!>y<!> = <!USELESS_PARENTHESES!>(true || false)<!>

    (x++)[x]
    (+x)[x]
    (x : Foo)[x]
    (x * x)[x]
    (x..x)[x]
    (x plus x)[x]
    (<!USELESS_ELVIS!>x<!> ?: x)[x]

    (x <!USELESS_CAST!>as<!> Foo) < x
    (x: Foo) < x

    +(+x)

    <!USELESS_PARENTHESES!>(@a{})<!>
    if (x < 0) return (@a{})
    else return <!USELESS_PARENTHESES!>(x)<!>
}

