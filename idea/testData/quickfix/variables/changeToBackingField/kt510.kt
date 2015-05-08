// "Change reference to backing field" "true"
public open class Identifier() {
    var field : Boolean
    set(v) {}
    init {
        <caret>this.field = false;
    }
}