// WITH_RUNTIME

class <caret>NotificationItemTypeModel {
    companion object Named {
        const val ABC = "ABC"
        const val CDE = "CDE"
    }
}

fun main() {
    println(NotificationItemTypeModel.ABC)
    println(NotificationItemTypeModel.Named.ABC)
}
