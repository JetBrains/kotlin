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
define void @ByteArray.clone(%class.ByteArray**  %instance, %class.ByteArray*  %classvariable.this) 
{
%var12 = getelementptr inbounds %class.ByteArray* %classvariable.this, i32 0, i32 0
%var13 = load i32* %var12, align 4
%var15 = call i8* @malloc(i32 8)
%var14 = bitcast i8* %var15 to %class.ByteArray*
%var17 = call i8* @malloc(i32 4)
%var16 = bitcast i8* %var17 to %class.ByteArray**
store %class.ByteArray* %var14, %class.ByteArray** %var16, align 4
call void @ByteArray(%class.ByteArray* %var14, i32 %var13)
%managed.index.1 = alloca i32, align 4
store i32 0, i32* %managed.index.1, align 4
br label %label.while.1
label.while.1:
%var18 = getelementptr inbounds %class.ByteArray* %classvariable.this, i32 0, i32 0
%var19 = load i32* %managed.index.1, align 4
%var20 = load i32* %var18, align 4
%var21 = icmp slt i32 %var19, %var20
br i1 %var21, label %label.while.2, label %label.while.3
label.while.2:
%var22 = load i32* %managed.index.1, align 4
%var23 = call i8 @ByteArray.get_Int(%class.ByteArray* %classvariable.this, i32 %var22)
%var24 = alloca i8, align 1
store i8 %var23, i8* %var24, align 1
%var25 = load %class.ByteArray** %var16, align 4
%var26 = load i32* %managed.index.1, align 4
%var27 = load i8* %var24, align 1
call void @ByteArray.set_Int_Byte(%class.ByteArray* %var25, i32 %var26, i8 %var27)
%var28 = load i32* %managed.index.1, align 4
%var29 = add nsw i32 %var28, 1
%var30 = load i32* %managed.index.1, align 4
store i32 %var29, i32* %managed.index.1, align 4
br label %label.while.1
label.while.3:
%var31 = load %class.ByteArray** %var16, align 4
store %class.ByteArray* %var31, %class.ByteArray** %instance, align 4
ret void 
}
define i8 @ByteArray.get_Int(%class.ByteArray*  %classvariable.this, i32  %index) 
{
%index.addr = alloca i32, align 4
store i32 %index, i32* %index.addr, align 4
%var32 = getelementptr inbounds %class.ByteArray* %classvariable.this, i32 0, i32 1
%var33 = load i32* %var32, align 4
%var34 = load i32* %index.addr, align 4
%var35 = call i8 @kotlinclib_get_byte(i32 %var33, i32 %var34)
%var36 = alloca i8, align 1
store i8 %var35, i8* %var36, align 1
%var37 = load i8* %var36, align 1
ret i8 %var37
}
define void @ByteArray.set_Int_Byte(%class.ByteArray*  %classvariable.this, i32  %index, i8  %value) 
{
%index.addr = alloca i32, align 4
store i32 %index, i32* %index.addr, align 4
%value.addr = alloca i8, align 1
store i8 %value, i8* %value.addr, align 1
%var38 = getelementptr inbounds %class.ByteArray* %classvariable.this, i32 0, i32 1
%var39 = load i32* %var38, align 4
%var40 = load i32* %index.addr, align 4
%var41 = load i8* %value.addr, align 1
call void @kotlinclib_set_byte(i32 %var39, i32 %var40, i8 %var41)
ret void 
}
%class.WithFields = type { i32, i32 }
define void @WithFields(%class.WithFields*  %classvariable.this, i32  %i) 
{
%classvariable.this.addr = alloca %class.WithFields, align 4
%i.addr = alloca i32, align 4
store i32 %i, i32* %i.addr, align 4
%var42 = load i32* %i.addr, align 4
%var43 = getelementptr inbounds %class.WithFields* %classvariable.this.addr, i32 0, i32 0
store i32 %var42, i32* %var43, align 4
%var44 = bitcast %class.WithFields* %classvariable.this to i8*
%var45 = bitcast %class.WithFields* %classvariable.this.addr to i8*
call void @llvm.memcpy.p0i8.p0i8.i64(i8* %var44, i8* %var45, i64 8, i32 4, i1 false)
%var46 = getelementptr inbounds %class.WithFields* %classvariable.this, i32 0, i32 1
%var47 = getelementptr inbounds %class.WithFields* %classvariable.this, i32 0, i32 0
%var48 = load i32* %var46, align 4
%var49 = load i32* %var47, align 4
store i32 %var49, i32* %var46, align 4
ret void 
}
define i32 @test_field_assignment_Int(i32  %i) 
{
%i.addr = alloca i32, align 4
store i32 %i, i32* %i.addr, align 4
%var50 = load i32* %i.addr, align 4
%var52 = call i8* @malloc(i32 8)
%var51 = bitcast i8* %var52 to %class.WithFields*
%var54 = call i8* @malloc(i32 4)
%var53 = bitcast i8* %var54 to %class.WithFields**
store %class.WithFields* %var51, %class.WithFields** %var53, align 4
call void @WithFields(%class.WithFields* %var51, i32 %var50)
%var55 = load %class.WithFields** %var53, align 4
%var56 = getelementptr inbounds %class.WithFields* %var55, i32 0, i32 1
%var57 = load i32* %var56, align 4
ret i32 %var57
}
define i32 @test_simple_field() 
{
%var59 = call i8* @malloc(i32 8)
%var58 = bitcast i8* %var59 to %class.WithFields*
%var61 = call i8* @malloc(i32 4)
%var60 = bitcast i8* %var61 to %class.WithFields**
store %class.WithFields* %var58, %class.WithFields** %var60, align 4
call void @WithFields(%class.WithFields* %var58, i32 1)
%var62 = load %class.WithFields** %var60, align 4
%var63 = getelementptr inbounds %class.WithFields* %var62, i32 0, i32 1
%var64 = load i32* %var63, align 4
ret i32 %var64
}
declare i8 @kotlinclib_get_byte(i32  %src, i32  %index) 
declare i32 @malloc_array(i32  %size) 
declare void @kotlinclib_set_byte(i32  %src, i32  %index, i8  %value) 

