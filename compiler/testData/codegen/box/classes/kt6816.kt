public class CalculatorConstants(
        val id: Long = 0,
        val detour: Double = 0.0,
        val taxi: Double = 0.0,
        val loop: Double = 0.0,
        val planeCondition: Double = 0.0,
        val co2PerKerosene: Double = 0.0,
        val freight: Double = 0.0,
        val rfi: Double = 0.0,
        val rfiAltitude: Double = 0.0,
        val averageContribution: Double = 0.0,
        val singleContribution: Double = 0.0,
        val returnContribution: Double = 0.0,
        val defraFactor: Double = 0.0,
        val airCondMult: Double = 0.0,
        val autoTransMult: Double = 0.0,
        val hybridDefault: String? = null,
        val travelClassOne: Double = 0.0,
        val status: String = "OK"
)

fun box(): String {
    val c = CalculatorConstants()
    return c.status
}
