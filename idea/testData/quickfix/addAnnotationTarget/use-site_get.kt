// "Add annotation target" "true"
@Target
annotation class GetAnn

class Get(<caret>@get:GetAnn val foo: String)