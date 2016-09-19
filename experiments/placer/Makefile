CXXFLAGS = -std=c++11 -g2 -Wall -Werror
CC=$(CXX) #change linker to c++

all: placer

run: placer
	./placer
clean:
	$(RM) *.o placer

placer: placer.o

place.o: placer.cc layout.h
	$(CXX) $(CXXFLAGS) -c placer.cc

.PHONY: all run clean
