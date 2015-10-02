annotation class AnnProp
annotation class AnnField
annotation class AnnProp2
annotation class AnnGetter
annotation class AnnSetter
annotation class AnnParam

public class A(@AnnParam @field:AnnField @property:AnnProp2 val x: Int, @param:AnnParam @get:AnnGetter @set:AnnSetter var y: Int) {

    @AnnProp @field:AnnField @property:AnnProp2 @get:AnnGetter @set:AnnSetter @setparam:AnnParam
    var p: Int = 0

}