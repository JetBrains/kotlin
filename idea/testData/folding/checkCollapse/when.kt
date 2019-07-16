fun test(i: Int) <fold text='{...}' expand='true'>{
    when (i) <fold text='{...}' expand='true'>{
        1 -> println(1)
        2 -> println(2)
        else -> println(0)
    }</fold>
}</fold>