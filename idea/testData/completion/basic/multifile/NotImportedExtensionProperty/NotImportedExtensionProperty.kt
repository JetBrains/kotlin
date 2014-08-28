package first

fun firstFun() {
    val a = ""
    a.hello<caret>
}

// EXIST: helloProp1
// EXIST: helloProp2
// ABSENT: helloProp3
// ABSENT: helloProp4
// NUMBER: 2