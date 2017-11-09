package test;

public class CyclicDependencies {

    public KotlinClass useKotlinClass() {
        return new KotlinClass().getKotlinClass();
    }

    public KotlinClass2 useKotlinClass2(KotlinClass kotlinClass) {
        return new KotlinClass2();
    }

}
