declare void @llvm.memcpy.p0i8.p0i8.i64(i8* nocapture, i8* nocapture readonly, i64, i32, i1)
declare i8* @malloc_static(i32)
attributes #0 = { nounwind "stack-protector-buffer-size"="8" "target-cpu"="cortex-m3" "target-features"="+hwdiv,+strict-align" }
%class.ComplexRef = type { i32, %class.Simple* }
define void @ComplexRef(%class.ComplexRef*  %classvariable.this, i32  %i, %class.Simple*  %s) #0
{
%classvariable.this.addr = alloca %class.ComplexRef, align 4
%i.addr = alloca i32, align 4
store i32 %i, i32* %i.addr, align 4
%var1 = load i32* %i.addr, align 4
%var2 = getelementptr inbounds %class.ComplexRef* %classvariable.this.addr, i32 0, i32 0
store i32 %var1, i32* %var2, align 4
%var3 = getelementptr inbounds %class.ComplexRef* %classvariable.this.addr, i32 0, i32 1
store %class.Simple* %s, %class.Simple** %var3, align 4
%var4 = bitcast %class.ComplexRef* %classvariable.this to i8*
%var5 = bitcast %class.ComplexRef* %classvariable.this.addr to i8*
call void @llvm.memcpy.p0i8.p0i8.i64(i8* %var4, i8* %var5, i64 8, i32 4, i1 false)
ret void 
}
%class.Simple = type { i32 }
define void @Simple(%class.Simple*  %classvariable.this, i32  %i) #0
{
%classvariable.this.addr = alloca %class.Simple, align 4
%i.addr = alloca i32, align 4
store i32 %i, i32* %i.addr, align 4
%var6 = load i32* %i.addr, align 4
%var7 = getelementptr inbounds %class.Simple* %classvariable.this.addr, i32 0, i32 0
store i32 %var6, i32* %var7, align 4
%var8 = bitcast %class.Simple* %classvariable.this to i8*
%var9 = bitcast %class.Simple* %classvariable.this.addr to i8*
call void @llvm.memcpy.p0i8.p0i8.i64(i8* %var8, i8* %var9, i64 4, i32 4, i1 false)
ret void 
}
define void @myFunction(%class.ComplexRef*  %ref) #0
{
%var10 = getelementptr inbounds %class.ComplexRef* %ref, i32 0, i32 0
%var11 = load i32* %var10, align 4
store i32 1, i32* %var10, align 4
ret void 
}
define i32 @const(i32  %i) #0
{
%i.addr = alloca i32, align 4
store i32 %i, i32* %i.addr, align 4
%var12 = load i32* %i.addr, align 4
ret i32 %var12
}
define void @kotlin_main() #0
{
%var13 = add nsw i32 3, 4
%var14 = call i32 @const(i32 %var13)
%var15 = alloca i32, align 4
store i32 %var14, i32* %var15, align 4
%var16 = load i32* %var15, align 4
%var17 = call i32 @const(i32 %var16)
%var18 = alloca i32, align 4
store i32 %var17, i32* %var18, align 4
ret void 
}

