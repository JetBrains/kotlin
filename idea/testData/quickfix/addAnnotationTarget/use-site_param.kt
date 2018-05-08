// "Add annotation target" "true"
@Target
annotation class ParamAnn

class Param(<caret>@param:ParamAnn val foo: String)