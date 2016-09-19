CXXFLAGS = -std=c++11 -g2 -Wall -Werror
CC=$(CXX) #change linker to c++

all: placer

run: placer
	./placer
clean:
	$(RM) *.o placer

placer: placer.o

.PHONY: all run clean
