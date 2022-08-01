enum class Enum(val i: Int) {

    TEST(45),
    PROBE(45),
    SONDE(45);

    <expr>constructor(x: String) : this(x.length)</expr>

}