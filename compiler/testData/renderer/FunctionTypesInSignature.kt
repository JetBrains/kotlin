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

fun ((Int) -> Unit)?.f7() {
}

fun <T> (T.(Int) -> Unit).f8() {
}

val Function0<(() -> Unit) -> (() -> Unit)>.p: Unit = Unit
//package test
//internal fun (() -> kotlin.Unit).f1(f: () -> kotlin.Unit): () -> kotlin.Int defined in test
//value-parameter val f: () -> kotlin.Unit defined in test.f1
//internal fun (() -> kotlin.Unit).f2(f: () -> kotlin.Unit): () -> kotlin.Int defined in test
//value-parameter val f: () -> kotlin.Unit defined in test.f2
//internal fun ((() -> kotlin.Unit) -> kotlin.Unit).f3(f: () -> () -> kotlin.Unit): () -> () -> kotlin.Int defined in test
//value-parameter val f: () -> () -> kotlin.Unit defined in test.f3
//internal fun (() -> () -> kotlin.Unit).f4(f: (() -> kotlin.Unit, () -> kotlin.Unit) -> kotlin.Unit): (() -> kotlin.Unit) -> () -> kotlin.Unit defined in test
//value-parameter val f: (() -> kotlin.Unit, () -> kotlin.Unit) -> kotlin.Unit defined in test.f4
//internal fun (() -> (() -> kotlin.Unit) -> () -> kotlin.Unit).f5(): kotlin.Unit defined in test
//internal fun (() -> (() -> kotlin.Unit) -> () -> kotlin.Unit)?.f6(): kotlin.Unit defined in test
//internal fun ((kotlin.Int) -> kotlin.Unit)?.f7(): kotlin.Unit defined in test
//internal fun <T> (T.(kotlin.Int) -> kotlin.Unit).f8(): kotlin.Unit defined in test
//<T> defined in test.f8
//internal val (() -> (() -> kotlin.Unit) -> () -> kotlin.Unit).p: kotlin.Unit defined in test