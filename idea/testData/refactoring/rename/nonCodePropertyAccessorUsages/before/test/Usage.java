package test;

public class CommentUsage {
    public void context() {
        Used.usedVar = 0;
        Used.setUsedVar(0);
        Used.getUsedVar();
        Used.INSTANCE.usedVar = 0;
        Used.INSTANCE.setUsedVar(0);
        Used.INSTANCE.getUsedVar();
        //        Used.usedVar = 0;
        //        Used.setUsedVar(0);
        //        Used.getUsedVar();
        //        Used.INSTANCE.usedVar = 0;
        //        Used.INSTANCE.setUsedVar(0);
        //        Used.INSTANCE.getUsedVar();
        String v = "Used.usedVar"
                   + "Used.setUsedVar(0)"
                   + "Used.getUsedVar()"
                   + "Used.INSTANCE.usedVar"
                   + "Used.INSTANCE.setUsedVar(0)"
                   + "Used.INSTANCE.getUsedVar()";
    }
}