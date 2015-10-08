public class J extends A {

    public boolean okField = false;

    public int getValProp() {
        return 123;
    }

    public int getVarProp() {
        return 456;
    }

    public void setVarProp(int x) {
        okField = true;
    }

    public int isProp() {
        return 789;
    }

    public void setProp(int x) {
        okField = true;
    }
}
