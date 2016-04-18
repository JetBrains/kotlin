package custom;

public class CClass extends BClass {
    public AClass returnA() {}
    public void paramA(AClass a) {}
    @AAnnotation(AEnum.AX)
    public void annoA() {}
    public BClass returnB() {}
    public void paramB(BClass b) {}
    @BAnnotation
    public void annoB() {}
}