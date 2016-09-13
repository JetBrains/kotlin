package test

fun Function0<Unit>.f1(f: Function0<Unit>): Function0<Int> {
}

fun (() -> Unit).f2(f: Function0<Unit>) : Function0<Int> {
}

fun ((() -> Unit) -> Unit).f3(f: Function0<Function0<Unit>>) : Function0<Function0<Int>> {
}

fun Function0<() -> Unit>.f4(f: (() -> Unit, () -> Unit) -> Unit): Function1<() -> Unit, () -> Unit> {
}

fun Function0<(() -> Unit) -> (() -> Unit)>.f5() {
}

fun Function0<(() -> Unit) -> (() -> Unit)>?.f6() {
}

fun ((p: Int) -> Unit)?.f7() {
}

fun <T> (T.(Int) -> Unit).f8() {
}

val Function0<(() -> Unit) -> (() -> Unit)>.p: Unit = Unit
//package test
//public fun (() -> kotlin.Unit).f1(f: () -> kotlin.Unit): () -> kotlin.Int defined in test
//value-parameter f: () -> kotlin.Unit defined in test.f1
//public fun (() -> kotlin.Unit).f2(f: () -> kotlin.Unit): () -> kotlin.Int defined in test
//value-parameter f: () -> kotlin.Unit defined in test.f2
//public fun ((() -> kotlin.Unit) -> kotlin.Unit).f3(f: () -> () -> kotlin.Unit): () -> () -> kotlin.Int defined in test
//value-parameter f: () -> () -> kotlin.Unit defined in test.f3
//public fun (() -> () -> kotlin.Unit).f4(f: (() -> kotlin.Unit, () -> kotlin.Unit) -> kotlin.Unit): (() -> kotlin.Unit) -> () -> kotlin.Unit defined in test
//value-parameter f: (() -> kotlin.Unit, () -> kotlin.Unit) -> kotlin.Unit defined in test.f4
//public fun (() -> (() -> kotlin.Unit) -> () -> kotlin.Unit).f5(): kotlin.Unit defined in test
//public fun (() -> (() -> kotlin.Unit) -> () -> kotlin.Unit)?.f6(): kotlin.Unit defined in test
//public fun ((p: kotlin.Int) -> kotlin.Unit)?.f7(): kotlin.Unit defined in test
//public fun <T> (T.(kotlin.Int) -> kotlin.Unit).f8(): kotlin.Unit defined in test
//<T> defined in test.f8
//public val (() -> (() -> kotlin.Unit) -> () -> kotlin.Unit).p: kotlin.Unit defined in test