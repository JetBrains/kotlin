import foo.A

fun main(a: A) {
    a.field<error descr="[UNSAFE_CALL] Only safe (?.) or non-null asserted (!!.) calls are allowed on a nullable receiver of type String?">.</error>length
}
