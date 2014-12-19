package d

<!ILLEGAL_MODIFIER!>override<!> val f : ()-> Int = { 12 }

fun test() {
    f()
}

var g: Int = 1
    <!PACKAGE_MEMBER_CANNOT_BE_PROTECTED!>protected<!> set(i: Int) {}