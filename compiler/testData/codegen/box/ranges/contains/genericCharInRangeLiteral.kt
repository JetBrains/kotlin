class Cell<T>(val value: T)

fun box(): String =
    if (Cell('a').value in 'a'..'z')
        "OK"
    else
        "fail"