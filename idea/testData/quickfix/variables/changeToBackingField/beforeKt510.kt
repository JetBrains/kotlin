// "Change reference to backing field" "true"
public open class Identifier() {
    var field : Boolean
    {
        <caret>this.field = false;
    }
}