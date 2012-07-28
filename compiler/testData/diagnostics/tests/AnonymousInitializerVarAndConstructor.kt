// http://youtrack.jetbrains.net/issue/KT-419

class A(w: Int) {
    var c = w

    {
        c = 81
    }
}

