package test

data public class RemData(val remo<caret>vable: Int)

fun usage(data: RemData): Int {
    return data.component1()
}
