#!/bin/bash
clr='\033[1;33m'
red='\033[0;31m'
nc='\033[0m'

# ============= Building ================
# Build Proto Compiler
echo -e "${clr}Building protoc compiler${nc}"
cd proto/compiler
echo -e "${clr}Checking prerequisites for building protoc${nc}"
./pre-build.sh
if [ $? -ne 0 ]; then
	echo -e "${red}Error launching carkot/proto/compiler/pre-build.sh${nc}"
	exit 1
fi

echo -e "${clr}Building protoc${nc}"
make
if [ $? -ne 0 ]; then
	echo -e "${red}Error building protoc with carkot/proto/compiler/Makefile ${nc}"
	exit 1
fi
cd ../../

# Compile proto-files
echo -e "${clr}Compiling proto-files sources${nc}"
cd proto/protofiles_sources
./compile_proto.sh
if [ $? -ne 0 ]; then
	echo -e "${red}Error compiling proto-files with carkot/proto/protofiles_sources/compile_proto.sh${nc}"
	exit 1
fi
cd ../../

# Build central server
echo -e "${clr}Building central server${nc}"
cd server
./gradlew build
if [ $? -ne 0 ]; then
	echo -e "${red}Error building central server with carkot/server/gradlew build${nc}"
	exit 1
fi
cd ../

# Build Raspberry server
echo -e "${clr}Building Raspberry Pi server${nc}"
cd car_srv/kotlinSrv
./build.sh
if [ $? -ne 0 ]; then
	echo -e "${red}Error building Raspberry Pi server with carkot/car_srv/kotlinSrv/build.sh${nc}"
	exit 1
fi
cd ../../

# Build Kotlin->Native translator
echo -e "${clr}Building translator${nc}"
cd translator
./gradlew jar
if [ $? -ne 0 ]; then
	echo -e "${red}Error building translator with translator/gradlew jar${nc}"
	exit 1
fi
cd ../

# Build Kotstd libary for translator
echo -e "${clr}Building Kotstd${nc}"
cd kotstd
make
if [ $? -ne 0 ]; then
	echo -e "${red}Error building Kotlin STDlib with kotstd/Makefile${nc}"
	exit 1
fi



