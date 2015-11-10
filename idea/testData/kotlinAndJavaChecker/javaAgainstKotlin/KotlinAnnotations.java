class KotlinAnnotations {

    @<error descr="'d' missing though required">k.Anno1</error>()
    @<error descr="'c', 'g' missing though required">k.Anno2</error>()
    public static void m1() {
    }

    @<error descr="'d' missing though required">k.Anno1</error>(c = 3)
    @<error descr="'g' missing though required">k.Anno2</error>(c = 3)
    public static void m2() {
    }

    @k.Anno1(d = 5)
    @<error descr="'c' missing though required">k.Anno2</error>(g = "asdas")
    public static void m3() {
    }

    @k.Anno1(c = 1, d = 5)
    @k.Anno2(c = {6, 5}, g = "asdas")
    public static void m4() {
    }

    @k.Anno1(<error descr="Cannot resolve method 'x'">x</error> = 1)
    @k.Anno2(<error descr="Cannot resolve method 'x'">x</error> = 2)
    public static void m5() {
    }
}