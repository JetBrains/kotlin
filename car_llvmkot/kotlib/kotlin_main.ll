declare void @llvm.memcpy.p0i8.p0i8.i64(i8* nocapture, i8* nocapture readonly, i64, i32, i1)
declare i8* @malloc(i32)
%class.ByteArray = type { i32, i32 }
define void @ByteArray(%class.ByteArray*  %classvariable.this, i32  %size) 
{
%classvariable.this.addr = alloca %class.ByteArray, align 4
%size.addr = alloca i32, align 4
store i32 %size, i32* %size.addr, align 4
%var1 = load i32* %size.addr, align 4
%var2 = getelementptr inbounds %class.ByteArray* %classvariable.this.addr, i32 0, i32 0
store i32 %var1, i32* %var2, align 4
%var3 = bitcast %class.ByteArray* %classvariable.this to i8*
%var4 = bitcast %class.ByteArray* %classvariable.this.addr to i8*
call void @llvm.memcpy.p0i8.p0i8.i64(i8* %var3, i8* %var4, i64 8, i32 4, i1 false)
%var5 = getelementptr inbounds %class.ByteArray* %classvariable.this, i32 0, i32 1
%var6 = getelementptr inbounds %class.ByteArray* %classvariable.this, i32 0, i32 0
%var7 = load i32* %var6, align 4
%var8 = call i32 @malloc_array(i32 %var7)
%var9 = alloca i32, align 4
store i32 %var8, i32* %var9, align 4
%var10 = load i32* %var5, align 4
%var11 = load i32* %var9, align 4
store i32 %var11, i32* %var5, align 4
ret void 
}
define i8 @ByteArray.get(%class.ByteArray*  %classvariable.this, i32  %index) 
{
%index.addr = alloca i32, align 4
store i32 %index, i32* %index.addr, align 4
%var12 = getelementptr inbounds %class.ByteArray* %classvariable.this, i32 0, i32 1
%var13 = load i32* %var12, align 4
%var14 = load i32* %index.addr, align 4
%var15 = call i8 @kotlinclib_get_byte(i32 %var13, i32 %var14)
%var16 = alloca i8, align 1
store i8 %var15, i8* %var16, align 1
%var17 = load i8* %var16, align 1
ret i8 %var17
}
define void @ByteArray.clone(%class.ByteArray**  %instance, %class.ByteArray*  %classvariable.this) 
{
%var18 = getelementptr inbounds %class.ByteArray* %classvariable.this, i32 0, i32 0
%var19 = load i32* %var18, align 4
%var21 = call i8* @malloc(i32 8)
%var20 = bitcast i8* %var21 to %class.ByteArray*
call void @ByteArray(%class.ByteArray* %var20, i32 %var19)
%managed.index.1 = alloca i32, align 4
store i32 0, i32* %managed.index.1, align 4
br label %label.while.1
label.while.1:
%var22 = getelementptr inbounds %class.ByteArray* %classvariable.this, i32 0, i32 0
%var23 = load i32* %managed.index.1, align 4
%var24 = load i32* %var22, align 4
%var25 = icmp slt i32 %var23, %var24
br i1 %var25, label %label.while.2, label %label.while.3
label.while.2:
%var26 = load i32* %managed.index.1, align 4
%var27 = call i8 @ByteArray.get(%class.ByteArray* %classvariable.this, i32 %var26)
%var28 = alloca i8, align 1
store i8 %var27, i8* %var28, align 1
%var29 = load i32* %managed.index.1, align 4
%var30 = load i8* %var28, align 1
call void @ByteArray.set(%class.ByteArray* %var20, i32 %var29, i8 %var30)
%var31 = load i32* %managed.index.1, align 4
%var32 = add nsw i32 %var31, 1
%var33 = load i32* %managed.index.1, align 4
store i32 %var32, i32* %managed.index.1, align 4
br label %label.while.1
label.while.3:
store %class.ByteArray* %var20, %class.ByteArray** %instance, align 4
ret void 
}
define void @ByteArray.set(%class.ByteArray*  %classvariable.this, i32  %index, i8  %value) 
{
%index.addr = alloca i32, align 4
store i32 %index, i32* %index.addr, align 4
%value.addr = alloca i8, align 1
store i8 %value, i8* %value.addr, align 1
%var34 = getelementptr inbounds %class.ByteArray* %classvariable.this, i32 0, i32 1
%var35 = load i32* %var34, align 4
%var36 = load i32* %index.addr, align 4
%var37 = load i8* %value.addr, align 1
call void @kotlinclib_set_byte(i32 %var35, i32 %var36, i8 %var37)
ret void 
}
declare i8 @kotlinclib_get_byte(i32  %src, i32  %index) 
define i8 @bytearray_1(i8  %x) 
{
%x.addr = alloca i8, align 1
store i8 %x, i8* %x.addr, align 1
%var39 = call i8* @malloc(i32 8)
%var38 = bitcast i8* %var39 to %class.ByteArray*
call void @ByteArray(%class.ByteArray* %var38, i32 10)
%var40 = load i8* %x.addr, align 1
call void @ByteArray.set(%class.ByteArray* %var38, i32 1, i8 %var40)
%var41 = call i8 @ByteArray.get(%class.ByteArray* %var38, i32 1)
%var42 = alloca i8, align 1
store i8 %var41, i8* %var42, align 1
%var43 = load i8* %var42, align 1
ret i8 %var43
}
declare i32 @malloc_array(i32  %size) 
declare void @kotlinclib_set_byte(i32  %src, i32  %index, i8  %value) 

