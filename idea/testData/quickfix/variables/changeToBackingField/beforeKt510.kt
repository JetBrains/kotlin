// "Change reference to backing field" "true"
public open class Identifier() {
    var field : Boolean
    set(v) {}
    {
        <caret>this.field = false;
    }
}