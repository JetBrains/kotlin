package first

fun firstFun(p: String.() -> Unit) {
    p.hello<caret>
}

// EXIST: helloFun1
// ABSENT: helloFun2
// EXIST: helloFun3
// ABSENT: helloFun4
// ABSENT: helloFun5
// EXIST: helloAny
// NOTHING_ELSE: true
