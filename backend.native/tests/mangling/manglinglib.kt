public fun mangle1(l: List<Int>) {
    println("Int direct $l")
}

public fun mangle1(l: List<out Number>) {
    println("out Number direct $l")
}

public fun mangle1(l: List<*>) {
    println("star direct $l")
}

public fun <T> mangle2(l: T) where T: List<Int>
{
    println("Int param $l")
}

public fun <T> mangle2(l: T) where T: List<out Number>
{
    println("out Number param $l")
}

public fun <T> mangle2(l: T) where T: List<*>
{
    println("star param $l")
}

public fun <T> mangle3(l: T) 
{
    println("no constructors $l")       
}

public fun <T> mangle3(l: T) 
    where
        T: Comparable<T>
{
    println("single constructor $l")       
}

public fun <T> mangle3(l: T) 
    where
        T: Comparable<T>,
        T: Number
{
    println("two constructors $l")       
}

