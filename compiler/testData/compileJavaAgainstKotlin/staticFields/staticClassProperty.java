package test;

class staticClassProperty {

    public static void main(String[] args) {
        int i = Test.valProp;
        int j = Test.varProp;
        Test.varProp = 100;
    }
}
