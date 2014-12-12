fun <T> array(vararg t : T) : Array<T> = t as Array<T>

fun a() {
    <selection>array("a", "b")</selection>
}
/*
array
strings
*/