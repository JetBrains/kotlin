declare void @llvm.memcpy.p0i8.p0i8.i64(i8* nocapture, i8* nocapture readonly, i64, i32, i1)
declare i8* @malloc(i32)
%class.ComplexRef = type { i32, %class.Simple* }
define void @ComplexRef(%class.ComplexRef*  %classvariable.this, i32  %i, %class.Simple*  %s) 
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
define void @Simple(%class.Simple*  %classvariable.this, i32  %i) 
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
define void @main() 
{
%var11 = call i8* @malloc(i32 4)
%var10 = bitcast i8* %var11 to %class.Simple*
call void @Simple(%class.Simple* %var10, i32 5)
%managed.s.1 = alloca %class.Simple, align 4
%var12 = load %class.Simple* %var10, align 4
store %class.Simple %var12, %class.Simple* %managed.s.1, align 4
%var14 = call i8* @malloc(i32 4)
%var13 = bitcast i8* %var14 to %class.ComplexRef*
call void @ComplexRef(%class.ComplexRef* %var13, i32 1, %class.Simple* %managed.s.1)
%managed.i.1 = alloca %class.ComplexRef, align 4
%var15 = load %class.ComplexRef* %var13, align 4
store %class.ComplexRef %var15, %class.ComplexRef* %managed.i.1, align 4
ret void 
}

