class A1<
    x: String,
    y: String,
>

class F<x: String, y: String>

class F2<x: String, y
: String>

class B1<
    x: String,
    y: String
>

class C1<
    x: String,
    y: String,>

class D1<
    x: String,
    y: String
    ,>

class A2<
    x: String,
    y: String,
    z: String,
>

class B2<
    x: String,
    y: String,
    z: String
>

class C2<
    x: String,
    y: String,
    z: String,>

class D2<
    x: String,
    y: String,
    z: String
    ,>

class A3<
    x: String,
>

class B3<
    x: String
>

class C3<
    x: String,>

class D3<
    x: String
    ,>

class A4<
    x: String   ,
    y: String,
    z   ,
>

class B4<
    x: String,
    y,
    z: String
>

class C4<
    x: String,
    y: String,
    z: String   ,>

class D4<
    x: String,
    y,
    z: String
    ,   >

class E1<
    x, y: String,
    z: String
    ,   >

class E2<
    x: String, y: String, z: String
>

class C<
    z: String, v

>

fun <x: String,
    y: String,
> a1() = Unit

fun <x: String,
    y: String
> b1() = Unit

fun <
    x: String,
    y: String,> c1() = Unit

fun <
    x: String,
    y: String
    ,> d1() = Unit

fun <
    x: String,
    y: String,
    z: String,
> a2() = Unit

fun <
    x: String,
    y: String,
    z: String
> b2() = Unit

fun <
    x: String,
    y: String,
    z: String,> c2() = Unit

fun <
    x: String,
    y: String,
    z: String
    ,> d2() = Unit

fun <
    x: String,
> a3() = Unit

fun <
    x: String
> b3() = Unit

fun <
    x: String,> c3() = Unit

fun <
    x: String
    ,> d3() = Unit

fun <
    x: String
    ,
    y: String,
    z: String   ,
> a4() = Unit

fun <
    x: String,
    y: String,
    z: String
> b4() = Unit

fun <x: String,
       y: String,
       z: String   ,> c4() = Unit

fun <
    x: String,
    y: String,
    z: String
    ,   > d4() = Unit

fun <
    x
> foo() {
}

fun <T> ag() {

}

fun <T,> ag() {

}

fun <
        T> ag() {

}

class C<
    x: Int
>

class G<
    x: String, y: String
    , /* */ z: String
>

class G<
    x: String, y: String
    /* */, /* */ z: String
>()

class H<
    x: String, /*
    */ y: String,
    z: String   ,>

class J<
    x: String, y: String , z: String /*
     */
    ,   >

class K<
    x: String, y: String,
    z: String
    ,   >

class L<
    x: String, y: String, z: String
>

// SET_TRUE: ALLOW_TRAILING_COMMA
