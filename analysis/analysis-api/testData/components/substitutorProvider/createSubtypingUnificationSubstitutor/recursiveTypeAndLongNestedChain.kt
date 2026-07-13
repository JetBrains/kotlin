// False positive due to KT-86935
interface Base<T>

fun <T: Base<T>> usage(left: Base<Base<Base<Int>>>, right: Base<T>) {
    l<caret_1_left>eft
    ri<caret_1_right>ght
}
