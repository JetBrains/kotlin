// FIR_IDENTICAL
//KT-2317 Wrong UNNECESSARY_SAFE_CALL

package kt2317

fun Any?.baz() = 1

fun foo(l: Long?) = l?.baz()


fun Any?.bar(): Unit { }

fun quux(x: Int?): Unit {
	x?.baz()
	x?.bar()
}
