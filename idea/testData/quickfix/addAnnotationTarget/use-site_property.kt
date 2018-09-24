// "Add annotation target" "true"
@Target
annotation class PropertyAnn

class Property(<caret>@property:PropertyAnn val foo: String)