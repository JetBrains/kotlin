// "Remove 'val' from parameter" "true"
class UsedInProperty(private <caret>val x: Int) {
    var y: String

    init {
        y = x.toString()
    }
}
