package _06_classy._53_Do_Your_Thing;

public class MyThing extends Thing {
    private final int arg;

    /*
     * This constructor is illegal. Rewrite it so that it has the same
     * effect but is legal.
     */
    public MyThing() {
        this((int)System.currentTimeMillis());
    }

    /*
    * This constructor is illegal. Rewrite it so that it has the same
    * effect but is legal.
    */
    public MyThing(int arg) {
        super(arg);
        this.arg = arg;
    }


}
